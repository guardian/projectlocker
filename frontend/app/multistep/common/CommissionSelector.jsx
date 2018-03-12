import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import ErrorViewComponent from "./ErrorViewComponent.jsx";

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
            error: null
        }
    }

    loadData(){
        this.setState({loading:true, error: null, commissionList:[]},()=>{
            const searchDoc = {workingGroupId: this.props.workingGroupId, status: this.props.showStatus, match: "W_EXACT" };
            axios.put("/api/pluto/commission/list", searchDoc).then(response=>{
                this.setState({loading: false, error: null, commissionList: response.data.result})
            }).catch(error=>this.setState({loading: false, error: error}));
        })
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
                           onChange={event=>this.props.valueWasSet(event.target.value)}
                           defaultValue={this.props.selectedCommissionId}>
                {
                    this.state.commissionList.map(comm=><option key={comm.id} value={comm.id}>{comm.title}</option>)
                }
                </select>;
    }
}

export default CommissionSelector;