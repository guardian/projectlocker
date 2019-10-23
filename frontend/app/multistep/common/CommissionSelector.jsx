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

    /**
     * this method reads the next item from the NDJSON stream, adds it to the state and then recurses (from a callback)
     * to get the next one.
     * when the stream is complete, we clear the flags and stop "recursing"
     * it is not a "true" recursion, of course, because it just schedules callback functions to take place
     * when data is available.
     * @param reader an ndjsonStream.Reader instance that yields us javascript objects
     */
    iterateStream(reader) {
        reader.read().then(({done, value}) => {
            if (done) {
                console.log("Streaming in commissions completed");
                this.setState({loading: false, error: null});
                return;
            }

            this.setState(state => ({...state,
                commissionList: state.commissionList.concat(value),
                selectedCommissionId: state.selectedCommissionId ? state.selectedCommissionId : value.id
            }), () => this.iterateStream(reader))
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
                .then(response =>{
                    console.log("got response, converting to stream");
                    console.log(response);
                    //console.log(Object.getPrototypeOf(response.body));
                    return ndjsonStream(response.body)
                })
                .then(commissionStream => {
                    //see https://davidwalsh.name/streaming-data-fetch-ndjson
                    //also https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream
                    console.log("got response, retrieving reader");
                    const reader = commissionStream.getReader();
                    this.iterateStream(reader); //"recursively" read the stream until there is no data left
                }).catch(error => {
                    console.error(error);
                    this.setState({loading: false, error: error})
                })
        })
    }

    componentDidUpdate(prevProps, prevState){
        if(prevProps.workingGroupId!==this.props.workingGroupId || prevProps.showStatus!==this.props.showStatus)
            this.loadData();
    }

    render(){
        if(this.state.error)
            return <ErrorViewComponent error={this.state.error}/>;
        else
            return <span><select id="commission-selector"
                           onChange={event=>this.props.valueWasSet(parseInt(event.target.value))}
                           defaultValue={this.props.selectedCommissionId}>
                {
                    this.state.commissionList.map(comm=><option key={comm.id} value={comm.id}>{comm.title}</option>)
                }
            </select><img src="/assets/images/uploading.svg" style={{display: this.props.loading ? "inline" : "none",height: "20px" }}/></span>;
    }
}

export default CommissionSelector;