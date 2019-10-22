import React from 'react';
import PropTypes from 'prop-types';
import ErrorViewComponent from "./ErrorViewComponent.jsx";
import ndjsonStream from 'can-ndjson-stream';

class CommissionSelector extends React.Component {
    static propTypes = {
        workingGroupId: PropTypes.number.isRequired,
        selectedCommissionId: PropTypes.number.isRequired,
        showStatus: PropTypes.string.isRequired,
        valueWasSet: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            commissionList: [],
            loading: false,
            error: null,
            selectedCommissionId: null
        };

        this.loadData = this.loadData.bind(this);
        this.iterateStream = this.iterateStream.bind(this);
    }

    UNSAFE_componentWillMount(){
        this.loadData();
    }

    iterateStream(reader) {
        reader.read().then(({done, value}) => {
            if (done) {
                console.log("Streaming in commissions completed");
                this.setState({loading: false, error: null});
                return;
            }

            this.setState({
                commissionList: this.state.commissionList.concat(value),
                selectedCommissionId: this.state.selectedCommissionId ? this.state.selectedCommissionId : value.id
            }, () => this.iterateStream(reader))
        });
    }

    loadData(){
        this.setState({loading:true, error: null, commissionList:[]},()=> {
            const searchDoc = {
                workingGroupId: this.props.workingGroupId,
                status: this.props.showStatus,
                match: "W_EXACT"
            };
            fetch("/api/pluto/commission/liststreamed", {
                method: "PUT",
                body: JSON.stringify(searchDoc),
                headers: {
                    "Content-Type": "application/json"
                }
            })
                .then(response => ndjsonStream(response.body))
                .then(commissionStream => {
                    //see https://davidwalsh.name/streaming-data-fetch-ndjson
                    //also https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream

                    // let read;
                    //
                    // commissionStream.getReader().read().then(read = result => {
                    //     if (result.done) {
                    //         console.log("Completed stream, terminating download");
                    //         this.setState({loading: false, error: null});
                    //         return;
                    //     }
                    //
                    //     console.log("Streamed in ", result.value);
                    //     this.setState({
                    //         commissionList: this.state.commissionList.concat(result.value),
                    //         selectedCommissionId: this.state.selectedCommissionId ? this.state.selectedCommissionId : result.value.id
                    //     }, ()=>commissionStream.getReader().read().then(read)) //recurse through the stream
                    // })

                    const reader = commissionStream.getReader();
                    this.iterateStream(reader);

                }).catch(error => {
                this.setState({loading: false, error: error})
            })
        })
            // axios.put("/api/pluto/commission/liststreamed", searchDoc).then(response=>{
            //     this.setState({loading: false,
            //         error: null,
            //         commissionList: response.data.result,
            //         selectedCommissionId:  response.data.result.length ? response.data.result[0].id : null
            //     },()=>{
            //         this.props.valueWasSet(this.state.selectedCommissionId);
            //     })
            //}).catch(error=>this.setState({loading: false, error: error}));
    }

    componentDidUpdate(prevProps, prevState){
        if(prevProps.workingGroupId!==this.props.workingGroupId || prevProps.showStatus!==this.props.showStatus)
            this.loadData();
    }

    render(){
        if(this.state.loading)
            return <img src="/assets/images/uploading.svg" style={{display: this.props.loading ? "inline" : "none",height: "20px" }}/>;
        else if(this.state.error)
            return <ErrorViewComponent error={this.state.error}/>;
        else
            return <select id="commission-selector"
                           onChange={event=>this.props.valueWasSet(parseInt(event.target.value))}
                           defaultValue={this.props.selectedCommissionId}>
                {
                    this.state.commissionList.map(comm=><option key={comm.id} value={comm.id}>{comm.title}</option>)
                }
                </select>;
    }
}

export default CommissionSelector;