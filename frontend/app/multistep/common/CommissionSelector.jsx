import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import ErrorViewComponent from "./ErrorViewComponent.jsx";

class CommissionSelector extends React.Component {
    static propTypes = {
        workingGroupId: PropTypes.number.isRequired,
        selectedCommissionId: PropTypes.number.isRequired,
        valueWasSet: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            commissionList: [],
            loading: false,
            error: null,
            onlyInProduction: false
        }
    }

    componentWillMount(){
        this.loadData();
    }

    loadData(){
        this.setState({loading:true, error: null, commissionList:[]},()=>{
            const status = this.state.onlyInProduction ? "In production" : null;
            const searchDoc = {workingGroupId: this.props.workingGroupId, status: status, match: "W_EXACT" };
            axios.put("/api/pluto/commission/list", searchDoc).then(response=>{
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
        if(prevProps.workingGroupId!==this.props.workingGroupId) {
            this.loadData();
        } else if(prevState.onlyInProduction!==this.state.onlyInProduction){
            this.loadData();
        }
    }

    render(){
        if(this.state.loading)
            return <img src="/assets/images/uploading.svg" style={{display: this.props.loading ? "inline" : "none",height: "20px" }}/>;
        else if(this.state.error)
            return <ErrorViewComponent error={this.state.error}/>;
        else
            return <div>
                <select id="commission-selector"
                           onChange={event=>this.props.valueWasSet(parseInt(event.target.value))}
                           defaultValue={this.props.selectedCommissionId}>
                {
                    this.state.commissionList.map(comm=><option key={comm.id} value={comm.id}>{comm.title}</option>)
                }
                </select>
                <input type="checkbox"
                       checked={this.state.onlyInProduction}
                       onChange={event=>this.setState({onlyInProduction: event.target.checked})}
                       id="only-show-production"
                       style={{marginLeft: "1em"}}
                />
                <label htmlFor="only-show-production" style={{display: "inline", marginLeft: "0.4em"}}>Only show commissions "In production"</label>

            </div>;
    }
}

export default CommissionSelector;