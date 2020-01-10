import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import ErrorViewComponent from "./ErrorViewComponent.jsx";
import FilterableList from "../../common/FilterableList.jsx";

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
            refreshCounter:0
        };

        this.makeSearchDoc = this.makeSearchDoc.bind(this);
    }

    loadData(){
        this.setState({loading:true, error: null, commissionList:[]},()=>{
            const searchDoc = {workingGroupId: this.props.workingGroupId, status: this.props.showStatus, match: "W_EXACT" };
            axios.put("/api/pluto/commission/list?length=150", searchDoc).then(response=>{
                this.setState({loading: false,
                    error: null,
                    commissionList: response.data.result,
                    selectedCommissionId:  response.data.result.length ? response.data.result[0].id : null
                },()=>{
                    this.props.valueWasSet(this.state.selectedCommissionId);
                })
            }).catch(error=>this.setState({loading: false, error: error}));
        })
    }

    componentDidUpdate(prevProps, prevState){
        if(prevProps.workingGroupId!==this.props.workingGroupId || prevProps.showStatus!==this.props.showStatus) this.setState({refreshCounter: this.state.refreshCounter+1})
    }

    static convertContent(contentList){
        console.log("received", contentList);

        return contentList.result.map(comm=>{return {name: comm.title, value: comm.id}})
    }

    makeSearchDoc(enteredText){
        return {title: enteredText, workingGroupId: this.props.workingGroupId, status: this.props.showStatus, match: "W_CONTAINS" };
    }

    render(){
        if(this.state.error)
            return <ErrorViewComponent error={this.state.error}/>;
        else
            return <FilterableList onChange={newValue=>this.props.valueWasSet(parseInt(newValue))}
                                   value={this.props.selectedCommissionId}
                                   size={10}
                                   unfilteredContentFetchUrl="/api/pluto/commission/list?length=150"
                                   unfilteredContentConverter={CommissionSelector.convertContent}
                                   makeSearchDoc={this.makeSearchDoc}
                                   triggerRefresh={this.state.refreshCounter}
                                   />
    }
}

export default CommissionSelector;