import React from 'react';
import PropTypes from 'prop-types';
import ErrorViewComponent from "../common/ErrorViewComponent.jsx";
import SummaryComponent from "./SummaryComponent.jsx";
import axios from "axios";
import moment from "moment";

class CommissionCompletionComponent extends React.Component {
    static propTypes = {
        workingGroupId: PropTypes.number,
        wgList: PropTypes.array,
        title: PropTypes.string
    };

    constructor(props) {
        super(props);

        this.state = {
            error: null,
            inProgress: false,
            createTime: moment().format("YYYY-MM-DD[T]HH:mm:ss.SSSZ"),
        };
        this.confirmClicked = this.confirmClicked.bind(this);
        this.requestContent = this.requestContent.bind(this);
    }

    requestContent() {
        return {
            siteId: "PX",
            collectionId: Math.floor((Math.random() * 10000) + 1),  //give it a random number between 1 and 10,000. if the number is already in use then a 409 is returned by the server and we recurse.
            title: this.props.title,
            status: "New",
            workingGroupId: this.props.workingGroupId,
            created: this.state.createTime,
            updated: this.state.createTime
        }
    }

    makeRequest() {
        return axios.request({method: "POST",url:"/api/prexit/commission",data:this.requestContent()}).then(()=>{
            this.setState({inProgress: false}, ()=>window.location.assign("/commission/"));
        }).catch(err=>{
            console.error(err);
            //see https://gist.github.com/fgilio/230ccd514e9381fafa51608fcf137253
            if(err.response){
                if(err.response.status===409) {
                    return this.makeRequest();  //recurse with a different collection id
                }
            } else {
                this.setState({inProgress: false, error: err});
            }
        });
    }

    confirmClicked(evt){
        this.setState({inProgress: true}, ()=>
            axios.request({method: "POST",url:"/api/prexit/commission",data:this.requestContent()}).then(()=>{
                this.setState({inProgress: false}, ()=>window.location.assign("/commission/"));
            }).catch(err=>{
                console.error(err);
                this.setState({inProgress: false, error: err});
            }))
    }

    render() {
        return <div>
            <h3>Create new commission</h3>
            <p className="information">We will create a new commission with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>

            <ErrorViewComponent error={this.state.error}/>

            <SummaryComponent commissionName={this.props.title} wgList={this.props.wgList} selectedWorkingGroupId={this.props.workingGroupId} createTime={this.state.createTime}/>
            <span style={{float: "right"}}>
                <button onClick={this.confirmClicked}
                        disabled={this.state.inProgress}
                        style={{color: this.state.inProgress ? "lightgrey" : "black"}}
                >Confirm</button>
            </span>
        </div>
    }
}

export default CommissionCompletionComponent;